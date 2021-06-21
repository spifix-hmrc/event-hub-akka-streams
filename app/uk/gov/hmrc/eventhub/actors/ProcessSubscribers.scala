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

import akka.actor.{Actor, Props, Terminated}
import uk.gov.hmrc.eventhub.actors.ProcessSubscribers._
import uk.gov.hmrc.eventhub.model.SubscriberWorkItem
import uk.gov.hmrc.eventhub.service.{PublishEventService, SubscriberEventService}
import uk.gov.hmrc.mongo.workitem.WorkItem

class ProcessSubscribers(subService: SubscriberEventService, pubService: PublishEventService) extends Actor {

  override def receive: Receive = {
    case GetEvents => context.actorOf(Props(new GetEvent(pubService)))
    case NewEvent(e) => println("processing new event")
      context.watch(context.actorOf(Props(new GetEvent(pubService))))
      context.watch(context.actorOf(Props(new SendEvent(subService, pubService, e))))
    case Terminated(_) => if (context.children.isEmpty) {
      println("all events processed")
      context.stop(self)
    }
  }
}

object ProcessSubscribers {
  case object GetEvents
  case class NewEvent(e: WorkItem[SubscriberWorkItem])
}
