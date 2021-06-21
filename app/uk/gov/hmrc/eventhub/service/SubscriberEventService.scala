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

import com.google.inject.Inject
import play.api.http.Status

import uk.gov.hmrc.eventhub.actors.SendEvent.{PermanentFailure, RetrySend, SendStatus, Sent}
import uk.gov.hmrc.eventhub.connector.EventConnector
import uk.gov.hmrc.eventhub.model.{SubscriberWorkItem}

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class SubscriberEventService @Inject()(eventConnector: EventConnector)(implicit ec: ExecutionContext){

  def sendEventToSubscriber(s: SubscriberWorkItem): Future[SendStatus] = {
    eventConnector.postEvent(s.event, s.subscriber.endpoint).map { r =>
      if (Status.isSuccessful(r.status)) Sent
      else if (Status.isClientError(r.status)) PermanentFailure
      else RetrySend
    }
  }

}
