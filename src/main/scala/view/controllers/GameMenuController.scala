package view.controllers

import controller.Messages
import controller.Messages.{ Input, PauseGame, ResumeGame }
import model.entities.towers.TowerTypes
import model.entities.towers.TowerTypes.TowerType
import model.entities.towers.Towers.Tower
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.control.{ Label, ToggleButton }
import scalafx.scene.layout.{ HBox, VBox }
import scalafx.scene.shape.Shape
import scalafxml.core.macros.sfxml
import view.render.Renders.single
import view.render.Renders.toSingle
import view.render.Rendering

trait ViewGameMenuController extends ViewController {
  def setup(): Unit
  def anyTowerSelected(): Boolean
  def unselectDepot(): Unit
}

@sfxml
class GameMenuController(
    val gameMenu: VBox,
    val inputButtons: HBox,
    val playButton: ToggleButton,
    val exitButton: ToggleButton,
    val gameStatus: VBox,
    val towerDepot: VBox,
    var send: Input => Unit)
    extends ViewGameMenuController {

  override def setup(): Unit = {
    setSpacing()
    setupButtons()
    setupTowerDepot()
  }

  override def setSend(reference: Messages.Input => Unit): Unit = send = reference

  override def anyTowerSelected(): Boolean =
    towerDepot.children.map(_.getStyleClass.contains("selected")).reduce(_ || _)

  override def unselectDepot(): Unit =
    towerDepot.children.foreach(_.getStyleClass.remove("selected"))

  private def setSpacing(): Unit = {
    val space: Double = 10.0
    gameMenu.setSpacing(space)
    towerDepot.setSpacing(space)
  }

  private def setupButtons(): Unit = {
    playButton.onMouseClicked = _ =>
      playButton.text.value match {
        case "Pause"  => send(PauseGame()); playButton.text = "Resume"
        case "Resume" => send(ResumeGame()); playButton.text = "Pause"
      }
    exitButton.onMouseClicked = _ => println("Stop") //(StopGame())
  }

  private def setupTowerDepot(): Unit =
    TowerTypes.values.foreach { towerValue =>
      val tower: Tower[_] = towerValue.asInstanceOf[TowerType[_]].tower
      val renderedTower: Shape = Rendering a tower as single
      val towerBox: HBox = new HBox(renderedTower)
      towerBox.styleClass += "towerBox"
      towerBox.setCursor(Cursor.Hand)
      towerBox.onMousePressed = _ => {
        if (!towerBox.styleClass.contains("selected")) {
          unselectDepot()
          towerBox.styleClass += "selected"
        } else {
          unselectDepot()
        }
        towerBox.setCursor(Cursor.ClosedHand)
      }
      towerBox.onMouseReleased = _ => towerBox.setCursor(Cursor.Hand)

      val towerLabel: Label = Label(towerValue.asInstanceOf[TowerType[_]].toString().toUpperCase)
      towerLabel.styleClass += "towerLabel"
      towerBox.children += towerLabel
      towerBox.setAlignment(Pos.CenterLeft)
      towerDepot.children.add(towerBox)
    }
}
