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
import uk.gov.hmrc.eventhub.actors.SendEvent._
import uk.gov.hmrc.eventhub.model.SubscriberWorkItem
import uk.gov.hmrc.eventhub.service.{PublishEventService, SubscriberEventService}
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.ExecutionContextExecutor

class SendEvent(subService: SubscriberEventService, pubService: PublishEventService, w: WorkItem[SubscriberWorkItem]) extends Actor {
  implicit val exec: ExecutionContextExecutor = context.dispatcher
  subService.sendEventToSubscriber(w.item) pipeTo self

  override def receive: Receive = {
    case Sent => println("sent the event")
      pubService.deleteEvent(w) pipeTo self
    case RetrySend => println("temporary failure to send the event")
      pubService.permanentlyFailed(w) pipeTo self
    case PermanentFailure => println("permanent failure cannot send the event")
      pubService.permanentlyFailed(w) pipeTo self
    case _: Status.Failure => println("failure")
      stop()
    case MarkedAsPermFailure => println("event marked as perm failure")
      stop()
    case FailedToMarkAsPermFailure => println("failed to perm fail event")
      stop()
    case DeleteEvent => println("processed event received by subscriber")
      stop()
    case FailedToDeleteEvent => println("error deleting completed event")
      stop()
  }
  def stop(): Unit = {
    context.stop(self)
  }
}

object SendEvent {
  //send to subscriber messages
  sealed abstract class SendStatus
  case object Sent extends SendStatus
  case object RetrySend extends SendStatus
  case object PermanentFailure extends SendStatus

  //retry messages

  //perm fail messages
  sealed abstract class PermFailureStatus
  case object MarkedAsPermFailure extends PermFailureStatus
  case object FailedToMarkAsPermFailure extends PermFailureStatus

  //delete event messages
  sealed abstract class DeleteEventStatus
  case object DeleteEvent extends DeleteEventStatus
  case object FailedToDeleteEvent extends DeleteEventStatus
}
