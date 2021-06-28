/*
 * Copyright 2020 HM Revenue & Customs
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

import play.core.PlayVersion.current
import play.sbt.PlayImport.ws
import sbt._


object AppDependencies {
  val compile = Seq(ws,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"      % "0.50.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-28" % "0.50.0",
    "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0",
    "com.github.pureconfig" %% "pureconfig" % "0.16.0"
  )
  val test = Seq(
    "com.typesafe.play"      %% "play-test"                % current          % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0"          % Test
  )
}
