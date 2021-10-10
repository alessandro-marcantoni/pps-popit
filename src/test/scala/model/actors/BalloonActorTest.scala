package model.actors

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import controller.GameLoop.GameLoopActor
import controller.Messages.{
  EntityUpdated,
  Input,
  ModelUpdated,
  Render,
  RenderEntities,
  RenderStats,
  Start,
  TickUpdate,
  Update,
  UpdateEntity
}
import model.actors.BalloonActorTest.{ dummyModel, testBalloon }
import model.entities.balloons.BalloonLives.Red
import model.entities.balloons.Balloons.Balloon
import model.maps.Grids.Grid
import model.maps.Tracks.Track
import model.stats.Stats.GameStats
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import utils.Constants
import utils.Constants.Entities.defaultPosition

import scala.language.postfixOps

object BalloonActorTest {
  var gameLoop: Option[ActorRef[Input]] = None
  var testBalloon: Balloon = Red balloon

  val dummyModel: ActorRef[Update] => Behavior[Update] = b =>
    Behaviors.setup { ctx =>
      Behaviors.receiveMessage {
        case TickUpdate(elapsedTime, replyTo) =>
          gameLoop = Some(replyTo)
          b ! UpdateEntity(elapsedTime, List(), ctx.self)
          Behaviors.same
        case EntityUpdated(entity, _) =>
          testBalloon = entity.asInstanceOf[Balloon]
          gameLoop.get ! ModelUpdated(List(), GameStats())
          Behaviors.same
        case _ => Behaviors.same
      }
    }
}

class BalloonActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {
  val balloonActor: ActorRef[Update] = testKit.spawn(BalloonActor(testBalloon))

  val model: ActorRef[Update] =
    testKit.spawn(dummyModel(balloonActor))
  val view: TestProbe[Render] = testKit.createTestProbe[Render]()
  val gameLoop: ActorRef[Input] = testKit.spawn(GameLoopActor(model, view.ref))

  "The balloon actor" when {
    "asked to update" should {
      "reply to the model which should contact the view" in {
        gameLoop ! Start()
        view expectMessage RenderStats(GameStats())
        view expectMessage RenderEntities(List())
      }
      "update the position of its balloon" in {
        (testBalloon position) should be !== defaultPosition
      }
    }
  }
}
