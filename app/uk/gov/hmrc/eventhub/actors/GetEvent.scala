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

package uk.gov.hmrc.eventhub.actors

import akka.actor.{Actor, Status}
import akka.pattern.pipe
import uk.gov.hmrc.eventhub.actors.GetEvent._
import uk.gov.hmrc.eventhub.model.SubscriberWorkItem
import uk.gov.hmrc.eventhub.service.PublishEventService
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.ExecutionContextExecutor

class GetEvent(publishEventService: PublishEventService) extends Actor {
  implicit val exec: ExecutionContextExecutor = context.dispatcher
  publishEventService.getEvent pipeTo self

  override def receive: Receive = {
    case NoNewEvents => println("No new events found")
      context.stop(self)
    case NewEvent(e) => println("found event")
      context.parent ! ProcessSubscribers.NewEvent(e)
      context.stop(self)
    case _: Status.Failure => println("Failed to delete")
      context.stop(self)
  }
}

object GetEvent {

  sealed abstract class GetEventStatus
  case object NoNewEvents extends GetEventStatus
  case class NewEvent(e: WorkItem[SubscriberWorkItem]) extends GetEventStatus
}
