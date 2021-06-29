/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.eventhub.subscription

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.stream.scaladsl.{RetryFlow, Source}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.actors.SendEvent
import uk.gov.hmrc.eventhub.model.{Subscriber, SubscriberWorkItem}
import uk.gov.hmrc.eventhub.repository.SubscriberQueueRepository
import uk.gov.hmrc.eventhub.service.PublishEventService
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object PushSubscription extends Logging {

  /**
   * Failed idempotent requests are automatically retried, see: https://doc.akka.io/docs/akka-http/current/client-side/host-level.html#retrying-a-request
   * For POST requests we implement retry with https://doc.akka.io/docs/akka/current/stream/operators/RetryFlow/withBackoff.html#retryflow-withbackoff
   * Attempts to pull from work item queue when downstream signals demand, emits as soon as there is an available work item
   */
  def subscriptionSource(
    subscriber: Subscriber,
    subscriberQueueRepository: SubscriberQueueRepository,
    publishEventService: PublishEventService
  )(implicit actorSystem: ActorSystem, executionContext: ExecutionContext, materializer: Materializer): Source[(SendEvent.CompletionStatus, WorkItem[SubscriberWorkItem]), NotUsed] = {
    def onPull: Unit => Future[Option[(Unit, WorkItem[SubscriberWorkItem])]] = _ =>
      subscriberQueueRepository
        .getEvent
        .flatMap {
          case None => onPull(())
          case Some(event) => Future.successful(Some(() -> event))
        }

    val workItemSource: Source[WorkItem[SubscriberWorkItem], NotUsed] =
      Source.unfoldAsync(())(onPull)

    val httpFlow = Http().cachedHostConnectionPool[WorkItem[SubscriberWorkItem]](
      subscriber.uri.authority.host.toString(),
      subscriber.uri.authority.port
    )

    val retryHttpFlow = RetryFlow.withBackoff(
      minBackoff = 100.millis,
      maxBackoff = 2.minutes,
      randomFactor = 0.2,
      maxRetries = 5,
      flow = httpFlow
    ){
      case (inputs@(_, _), (Success(resp), _)) =>
        val output = resp.status match {
          case StatusCodes.Success(_) | StatusCodes.ClientError(_) => None
          case _ => Some(inputs)
        }
        resp.entity.discardBytes()
        output
      case ((_, _), (Failure(e), workItem)) =>
        logger.error(s"exception pushing event: ${workItem.item.event} to: ${subscriber.uri}, will not retry", e)
        None
    }

    val responseHandler = handleResponse(_, subscriber, publishEventService)
    val tuple = requestTuple(subscriber, _)

    workItemSource
      .throttle(subscriber.elements, subscriber.per)
      .map(tuple)
      .via(retryHttpFlow)
      .mapAsync(parallelism = 8)(responseHandler)
  }

  def requestTuple(subscriber: Subscriber, subscriberWorkItem: WorkItem[SubscriberWorkItem]): (HttpRequest, WorkItem[SubscriberWorkItem]) = {
    HttpRequest.apply(
      method = HttpMethods.POST,
      uri = subscriber.uri,
      entity = HttpEntity(
        ContentTypes.`application/json`, 
        Json.toJson(subscriberWorkItem.item.event).toString()
      )
    ) -> subscriberWorkItem
  }

  def handleResponse(
    responseTuple: (Try[HttpResponse], WorkItem[SubscriberWorkItem]),
    subscriber: Subscriber,
    publishEventService: PublishEventService
  )(implicit executionContext: ExecutionContext): Future[(SendEvent.CompletionStatus, WorkItem[SubscriberWorkItem])] = responseTuple match {
    case (Failure(e), subscriberWorkItem) =>
      logger.error(s"could not push event: ${subscriberWorkItem.item.event} to: ${subscriber.uri}, marking as permanently failed.", e)
      publishEventService.permanentlyFailed(subscriberWorkItem).map(_ -> subscriberWorkItem)
    case (Success(response), subscriberWorkItem) => if(response.status.isFailure()){
      logger.error(s"failure: ${response.status} when pushing: ${subscriberWorkItem.item.event} to: ${subscriber.uri}.")
      publishEventService.permanentlyFailed(subscriberWorkItem).map(_ -> subscriberWorkItem)
    } else {
      publishEventService.deleteEvent(subscriberWorkItem).map(_ -> subscriberWorkItem)
    }
  }
}
