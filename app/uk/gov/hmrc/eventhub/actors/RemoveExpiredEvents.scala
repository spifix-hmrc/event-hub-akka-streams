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
import uk.gov.hmrc.eventhub.actors.RemoveExpiredEvents._
import uk.gov.hmrc.eventhub.service.PublishEventService
import akka.pattern.pipe

import scala.concurrent.ExecutionContextExecutor

class RemoveExpiredEvents(pubService: PublishEventService) extends Actor {
  implicit val exec: ExecutionContextExecutor = context.dispatcher

  override def receive: Receive = {
    case RemoveExpired => println("RemoveExpired")
      pubService.removeExpiredEvents pipeTo self
    case RemoveSuccess(n) => println(s"removed $n events")
      context.stop(self)
    case RemoveNone => println("remove no events")
      context.stop(self)
    case _: Status.Failure => println("Failed to delete")
      context.stop(self)
  }
}

object RemoveExpiredEvents {
  case object RemoveExpired
  sealed abstract class RemoveStatus
  case class RemoveSuccess(num: Long) extends RemoveStatus
  case object RemoveNone extends RemoveStatus
}
