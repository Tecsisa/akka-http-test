/*
 * Copyright 2015 Dennis Vriend
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

package com.github.dnvriend

import java.io.{ FileOutputStream, PipedOutputStream, PipedInputStream }

import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.io.{ InputStreamSource, OutputStreamSink }
import akka.stream.scaladsl.{ Sink, Source }
import akka.stream.io.Implicits._
import akka.util.ByteString
import com.github.dnvriend.domain.Person
import com.github.dnvriend.util.TimeUtil

import scala.concurrent.Future

trait Service extends Marshallers with GenericServices {

  def routes: Route =
    logRequestResult("akka-http-test") {
      path("") {
        redirect("person", StatusCodes.PermanentRedirect)
      } ~
        pathPrefix("person") {
          complete {
            Person("John Doe", 25)
          }
        } ~
        pathPrefix("ping") {
          complete {
            Ping(TimeUtil.timestamp)
          }
        } ~
        // https://gist.github.com/jrudolph/08d0d28e1eddcd64dbd0
        // curl -v -X POST --header "Transfer-Encoding: chunked" -F "file=@/Users/jvazquez/Desktop/2001.A.Space_Odyssey.mkv" localhost:8080/upload
        pathPrefix("upload") {
          entity(as[Multipart.FormData]) { (formData: Multipart.FormData) ⇒
            val fileNamesFuture = formData.parts.mapAsync(1) { p ⇒
              println(s"Got part. name: ${p.name} filename: ${p.filename}")
              println("Counting size...")
              @volatile var lastReport = System.currentTimeMillis()
              @volatile var lastSize = 0L
              def receiveChunk(counter: (Long, Long), chunk: ByteString): (Long, Long) = {
                val (olSize, oldChunks) = counter
                val newSize = olSize + chunk.size
                val newChunks = oldChunks + 1

                val now = System.currentTimeMillis()
                if (now > lastReport + 1000) {
                  val lastedTotal = now - lastReport
                  val bytesSinceLast = newSize - lastSize
                  val speedMBPS = bytesSinceLast.toDouble / 1000000 / lastedTotal * 1000

                  println(f"Already got $newChunks%7d chunks with total size $newSize%11d bytes avg chunksize ${newSize / newChunks}%7d bytes/chunk speed: $speedMBPS%6.2f MB/s")

                  lastReport = now
                  lastSize = newSize
                }
                (newSize, newChunks)
              }
              p.entity.dataBytes.runFold((0L, 0L))(receiveChunk).map {
                case (size, numChunks) ⇒
                  println(s"Size is $size")
                  (p.name, p.filename, size)
              }
            }.runFold(Seq.empty[(String, Option[String], Long)])(_ :+ _).map(_.mkString(", "))
            complete {
              fileNamesFuture
            }
          }
        } ~
        pathPrefix("connect") {
          entity(as[Multipart.FormData]) { (formData: Multipart.FormData) ⇒
            val fileNamesFuture = formData.parts.mapAsync(1) { p ⇒
              val pipedIn = new PipedInputStream()
              val pipedOut = new PipedOutputStream(pipedIn)
              val fos = new FileOutputStream("/Users/jvazquez/Desktop/2001_backup.mkv")
              p.entity.dataBytes.to(OutputStreamSink(() ⇒ pipedOut)).run()
              (InputStreamSource(() ⇒ pipedIn) to Sink.outputStream(() ⇒ fos)).run()
              Future(p.name)
            }.runFold(Seq.empty[String])(_ :+ _).map(_.mkString(", "))
            complete {
              fileNamesFuture
            }
          }
        }
    }
}

object SimpleServer extends App with Service with CoreServices {
  Http().bindAndHandle(routes, "0.0.0.0", 8080)
}
