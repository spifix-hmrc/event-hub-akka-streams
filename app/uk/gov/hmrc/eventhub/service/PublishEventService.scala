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
import uk.gov.hmrc.eventhub.actors.SubscribersQueueController.SendEvents
import uk.gov.hmrc.eventhub.model.{DuplicateEvent, Event, FoundSubscribers, MongoEvent, NoSubscribers, NoTopics, PublishEvent, PublishStatus, SaveError, Subscriber, SubscriberWorkItem, Topic}
import uk.gov.hmrc.eventhub.repository.{EventHubRepository, SubscriberQueueRepository}

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PublishEventService @Inject()(eventHubRepository: EventHubRepository,
                                    subQueueRepository: SubscriberQueueRepository,
                                    @Named("eventTopics") topics: Map[String, List[Topic]],
                                    @Named("event-actor") eventActor: ActorRef ) {


  def processEvent(topic: String, event: Event): Future[PublishStatus] =
    for {
      a <- isNewEventWithSubscibers(topic, event)
      b <- saveSubQueue(a, event)
    } yield {
      b match {
        case PublishEvent(l) => eventActor ! SendEvents(l, event)
        case _ => ()
      }
      b
    }

  def isNewEventWithSubscibers(topic: String, event: Event): Future[PublishStatus] = {
    topics.get(topic) match {
      case None => Future.successful(NoTopics)
      case Some(l) =>
        getSubscriberWorkItems(event, l.head.subscribers) match {
          case List() => Future.successful(NoSubscribers)
          case ls => eventHubRepository.findEventByMessageId(event.messageId).map {
            {
              case null => FoundSubscribers(ls)
              case _ => DuplicateEvent
            }
          }
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

  def saveSubQueue(status: PublishStatus, event: Event): Future[PublishStatus] = status match {
    case FoundSubscribers(v) =>
      eventHubRepository.saveEvent(event).flatMap{res =>
        if (res.wasAcknowledged()) {
          subQueueRepository.addSubscriberWorkItems(v).map(_ => PublishEvent(v))
        } else Future.successful(SaveError)
      }
    case _ => Future.successful(status)
  }

  def getSubscriberWorkItems(e: Event, ls: List[Subscriber]): List[SubscriberWorkItem] =
    ls map(SubscriberWorkItem(_, e))






}
