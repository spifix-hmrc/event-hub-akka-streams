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

package uk.gov.hmrc.eventhub.service

import akka.actor.ActorRef
import com.mongodb.client.result.InsertOneResult
import org.mongodb.scala.{Observer, SingleObservable}
import org.mongodb.scala.result.InsertOneResult
import uk.gov.hmrc.eventhub.actors.EventActor.{SendEvents}
import uk.gov.hmrc.eventhub.model.{DuplicateEvent, Event, FoundSubscribers, MongoEvent, NoSubscribers, PublishEvent, PublishStatus, SaveError, Subscriber}
import uk.gov.hmrc.eventhub.repository.EventHubRepository

import java.util.UUID
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PublishEventService @Inject()(eventHubRepository: EventHubRepository, @Named("eventSubscribers") eventSubscribers: Map[String, List[Subscriber]],
                                    @Named("event-actor") eventActor: ActorRef ) {

  def processEvent(topic: String, event: Event): Future[PublishStatus] =
    for {
      a <- isNewEventWithSubscibers(topic, event)
      b <- saveEvent(a, event)
    } yield {
      b match {
        case PublishEvent(l) => eventActor ! SendEvents(l, event)
        case _ => ()
      }
      b
    }




  def isNewEventWithSubscibers(topic: String, event: Event): Future[PublishStatus] =
    eventSubscribers.get(topic) match {
      case None => Future.successful(NoSubscribers)
      case Some(value) => eventHubRepository.findEventByMessageId(event.messageId).map {
        {
          case null => FoundSubscribers(value)
          case _ => DuplicateEvent
        }
      }
    }


  def saveEvent(status: PublishStatus, event: Event): Future[PublishStatus] = status match {
    case FoundSubscribers(v) => eventHubRepository.saveEvent(event).map { res =>
      if (res.wasAcknowledged()) PublishEvent(v)
      else SaveError
    }
    case _ => Future.successful(status)
  }


}
