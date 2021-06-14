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
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.service.EventService

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.workitem._

@Singleton
class EventHubController @Inject()(val controllerComponents: ControllerComponents,
                                   eventService: EventService) extends BaseController {

  def publishEvent(topic: String) = Action(parse.json) { implicit request =>
    println(s"pbu event topic $topic ${request.body}")
    val event = request.body.validate[Event]
    println(s"parsed the event $event")
    event.fold(
      errors => {
        BadRequest(Json.obj("message" -> JsError.toJson(errors)))
      },
      e => {
        eventService.processEvent(e)
        Ok(views.html.index())
      }
    )
  }

  def index = Action {
    println("index")
    Ok(views.html.index())
  }
}