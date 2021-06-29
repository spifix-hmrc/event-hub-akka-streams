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

package uk.gov.hmrc.eventhub.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, RetryFlow}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.model.{Subscriber, SubscriberWorkItem}
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
 * Failed idempotent requests are automatically retried, see: https://doc.akka.io/docs/akka-http/current/client-side/host-level.html#retrying-a-request
 * For POST requests we implement retry with https://doc.akka.io/docs/akka/current/stream/operators/RetryFlow/withBackoff.html#retryflow-withbackoff
 * TODO make back off and HTTP method configurable
 */
class SubscriberHttpFlow(subscriber: Subscriber)
                        (implicit actorSystem: ActorSystem, materializer: Materializer) extends Logging {
  private val httpFlow = Http().cachedHostConnectionPool[WorkItem[SubscriberWorkItem]](
    subscriber.uri.authority.host.toString(),
    subscriber.uri.authority.port
  )

  def flow: Flow[WorkItem[SubscriberWorkItem], (Try[HttpResponse], WorkItem[SubscriberWorkItem]), NotUsed] = {
    val retryFlow: Flow[(HttpRequest, WorkItem[SubscriberWorkItem]), (Try[HttpResponse], WorkItem[SubscriberWorkItem]), Http.HostConnectionPool] = RetryFlow.withBackoff(
      minBackoff = 100.millis,
      maxBackoff = 2.minutes,
      randomFactor = 0.2,
      maxRetries = 5,
      flow = httpFlow
    ) {
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

    Flow[WorkItem[SubscriberWorkItem]]
      .map(withRequest)
      .throttle(subscriber.elements, subscriber.per)
      .via(retryFlow)
  }

  def withRequest(subscriberWorkItem: WorkItem[SubscriberWorkItem]): (HttpRequest, WorkItem[SubscriberWorkItem]) = {
    HttpRequest.apply(
      method = HttpMethods.POST,
      uri = subscriber.uri,
      entity = HttpEntity(
        ContentTypes.`application/json`,
        Json.toJson(subscriberWorkItem.item.event).toString()
      )
    ) -> subscriberWorkItem
  }
}
