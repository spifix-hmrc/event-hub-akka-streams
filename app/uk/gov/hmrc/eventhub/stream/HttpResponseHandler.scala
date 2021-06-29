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

import akka.http.scaladsl.model.HttpResponse
import play.api.Logging
import uk.gov.hmrc.eventhub.actors.SendEvent
import uk.gov.hmrc.eventhub.model.{Subscriber, SubscriberWorkItem}
import uk.gov.hmrc.eventhub.service.PublishEventService
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait HttpResponseHandler {
  def handleResponse(
    responseTuple: (Try[HttpResponse], WorkItem[SubscriberWorkItem])
  )(implicit executionContext: ExecutionContext): Future[(SendEvent.CompletionStatus, WorkItem[SubscriberWorkItem])]
}

class EventPublishingHttpResponseHandler(
  subscriber: Subscriber,
  publishEventService: PublishEventService
) extends HttpResponseHandler with Logging {

  override def handleResponse(
    responseTuple: (Try[HttpResponse], WorkItem[SubscriberWorkItem])
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