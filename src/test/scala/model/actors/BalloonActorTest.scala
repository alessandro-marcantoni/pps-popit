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
  Start,
  TickUpdate,
  Update,
  UpdateEntity
}
import model.actors.BalloonActorTest.{ balloon, dummyModel }
import model.entities.balloons.BalloonType.Red
import model.entities.balloons.Balloons.Balloon
import model.entities.balloons.Constants.defaultPosition
import model.maps.Grids.Grid
import model.maps.Tracks.Track
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import utils.Constants

import scala.language.postfixOps

object BalloonActorTest {
  var gameLoop: Option[ActorRef[Input]] = None
  var balloon: Balloon = Red balloon

  val dummyModel: (ActorRef[Update], Track) => Behavior[Update] = (b, track) =>
    Behaviors.setup { ctx =>
      Behaviors.receiveMessage {
        case TickUpdate(elapsedTime, replyTo) =>
          gameLoop = Some(replyTo)
          b ! UpdateEntity(elapsedTime, List(), ctx.self, track)
          Behaviors.same
        case EntityUpdated(entity) =>
          balloon = entity.asInstanceOf[Balloon]
          gameLoop.get ! ModelUpdated(List())
          Behaviors.same
        case _ => Behaviors.same
      }
    }
}

class BalloonActorTest extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers {
  val balloonActor: ActorRef[Update] = testKit.spawn(BalloonActor(balloon))

  val model: ActorRef[Update] =
    testKit.spawn(
      dummyModel(balloonActor, Track(Grid(Constants.widthRatio, Constants.heightRatio)))
    )
  val view: TestProbe[Render] = testKit.createTestProbe[Render]()
  val gameLoop: ActorRef[Input] = testKit.spawn(GameLoopActor(model, view.ref))

  "The balloon actor" when {
    "asked to update" should {
      "reply to the model which should contact the view" in {
        gameLoop ! Start()
        view expectMessage RenderEntities(List())
      }
      "update the position of its balloon" in {
        (balloon position) should be !== defaultPosition
      }
    }
  }
}
