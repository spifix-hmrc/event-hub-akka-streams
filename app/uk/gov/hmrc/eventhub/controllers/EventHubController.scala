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

package uk.gov.hmrc.eventhub.controllers

import play.api.libs.json.{JsError, Json}
import play.api.mvc.{BaseController, ControllerComponents}
import uk.gov.hmrc.eventhub.model.{DuplicateEvent, Event, NoSubscribers, NoTopics, SaveError, Subscriber}
import uk.gov.hmrc.eventhub.service.PublishEventService

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventHubController @Inject()(val controllerComponents: ControllerComponents,
                                   eventService: PublishEventService)
                                  (implicit ec: ExecutionContext)extends BaseController {

  def publishEvent(topic: String) = Action.async(parse.json) { implicit request =>
    val event = request.body.validate[Event]
    event.fold(
      errors => {
        Future.successful(BadRequest(Json.obj("message" -> JsError.toJson(errors))))
      },
      e => {
        eventService.processEvent(topic, e).map{
          case SaveError => InternalServerError
          case NoTopics => NotFound("No such topic")
          case NoSubscribers => Created("No Subscribers to Topic")
          case DuplicateEvent => Created("Duplicate event")
          case _ => Created
        }
      }
    )
  }

  def testSubsriber(topic: String) = Action(parse.json) { implicit request =>
    println(s"received event for topic $topic, body ${request.body}")
    topic match {
      case "fail" => InternalServerError
      case _ => Accepted
    }
  }

  def index = Action {
    Ok(views.html.index())
  }
}