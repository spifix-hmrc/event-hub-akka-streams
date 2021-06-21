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

import uk.gov.hmrc.eventhub.actors.GetEvent.{GetEventStatus, NewEvent, NoNewEvents}
import uk.gov.hmrc.eventhub.actors.SendEvent
import uk.gov.hmrc.eventhub.model._
import uk.gov.hmrc.eventhub.repository.{EventHubRepository, SubscriberQueueRepository}
import uk.gov.hmrc.mongo.workitem.WorkItem

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class PublishEventService @Inject()(eventHubRepository: EventHubRepository,
                                    subQueueRepository: SubscriberQueueRepository,
                                    @Named("eventTopics") topics: Map[String, List[Topic]]
                                     ) {


  def processEvent(topic: String, event: Event): Future[PublishStatus] =
    for {
      a <- isNewEventWithSubscibers(topic, event)
      b <- saveSubQueue(a, event)
    } yield b


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

  def getEvent: Future[GetEventStatus] = {
    println("getting events from the workitem queue")
    subQueueRepository.getEvent.map {
      case None => NoNewEvents
      case Some(e) => NewEvent(e)
    }
  }

  def deleteEvent(e: WorkItem[SubscriberWorkItem]): Future[SendEvent.DeleteEventStatus] = {
    subQueueRepository.deleteEvent(e).map {
      case true => SendEvent.DeleteEvent
      case false => SendEvent.FailedToDeleteEvent
    }
  }

  def permanentlyFailed(e: WorkItem[SubscriberWorkItem]): Future[SendEvent.PermFailureStatus] =
    subQueueRepository.permanentlyFailed(e).map{
      case true => SendEvent.MarkedAsPermFailure
      case false => SendEvent.FailedToMarkAsPermFailure
    }


}
