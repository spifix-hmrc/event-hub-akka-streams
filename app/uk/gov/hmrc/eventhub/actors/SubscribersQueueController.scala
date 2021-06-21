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

import akka.actor._
import uk.gov.hmrc.eventhub.actors.ProcessSubscribers.GetEvents

import javax.inject._
import uk.gov.hmrc.eventhub.actors.RemoveExpiredEvents.RemoveExpired
import uk.gov.hmrc.eventhub.service.{PublishEventService, SubscriberEventService}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object SubscribersQueueController {
  def props = Props[SubscribersQueueController]

  case object ProcessSubscribers
  case object CleanupEvents
}

class SubscribersQueueController @Inject()(subService: SubscriberEventService, pubService: PublishEventService) extends Actor {
  import SubscribersQueueController._
  implicit val exec: ExecutionContextExecutor = context.dispatcher
  context.system.scheduler.scheduleWithFixedDelay(1.second, 2.minutes, self, ProcessSubscribers)
  context.system.scheduler.scheduleWithFixedDelay(1.second, 1.minute, self, CleanupEvents)

  def receive = {
    case ProcessSubscribers =>
      println("processing subscribers")
      context.actorOf(Props(new ProcessSubscribers(subService, pubService))) ! GetEvents

    case CleanupEvents =>
      println("Removing expired events")
      context.actorOf(Props(new RemoveExpiredEvents(pubService))) ! RemoveExpired
  }
}
