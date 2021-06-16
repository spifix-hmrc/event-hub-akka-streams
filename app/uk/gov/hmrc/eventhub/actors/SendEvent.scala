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
import uk.gov.hmrc.eventhub.model.{Event, Subscriber}
import uk.gov.hmrc.eventhub.service.SubscriberEventService

import scala.concurrent.ExecutionContextExecutor

class SendEvent(subService: SubscriberEventService, s: Subscriber, e: Event) extends Actor {
  implicit val exec: ExecutionContextExecutor = context.dispatcher
  subService.sendEventToSubscriber(s, e) pipeTo self

  override def receive: Receive = {
    case Sent => println("sent the event")
      stop()
    case FailedToSend => println("failed to send the event")
      stop()
    case _: Status.Failure => println("failure")
      stop()
  }
  def stop(): Unit = {
    context.stop(self)
  }
}

object SendEvent {
  abstract class SendStatus
  case object Sent extends SendStatus
  case object FailedToSend extends SendStatus
}
