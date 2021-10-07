package view.controllers

import controller.Messages
import controller.Messages.{ Input, Message, PauseGame, ResumeGame }
import model.entities.bullets.Bullets.Bullet
import model.entities.towers.TowerTypes
import model.entities.towers.TowerTypes.TowerType
import model.entities.towers.Towers.Tower
import model.maps.Cells.Cell
import model.stats.Stats.GameStats
import scalafx.application.Platform
import scalafx.geometry.Pos
import scalafx.scene.Cursor
import scalafx.scene.control.{ Label, ToggleButton }
import scalafx.scene.layout._
import scalafx.scene.shape.Shape
import scalafxml.core.macros.sfxml
import utils.Constants
import utils.Constants.Maps.outerCell
import view.render.Rendering
import view.render.Renders.{ single, toSingle }

import scala.concurrent.Future

trait ViewGameMenuController extends ViewController {
  def setup(): Unit
  def update(stats: GameStats): Unit
  def anyTowerSelected(): Boolean
  def unselectDepot(): Unit
  def fillTowerStatus(tower: Tower[_], cell: Cell): Unit
  def clearTowerStatus(): Unit
  def isPaused: Boolean
  def getSelectedTowerType[B <: Bullet]: TowerType[B]
}

@sfxml
class GameMenuController(
    val gameMenu: VBox,
    val playButton: ToggleButton,
    val exitButton: ToggleButton,
    val gameStatus: VBox,
    val statusUpperBox: HBox,
    val lifeLabel: Label,
    val statusLowerBox: HBox,
    val moneyLabel: Label,
    val towerDepot: VBox,
    val towerStatus: VBox,
    var currentCell: Cell = outerCell,
    var send: Input => Unit,
    var ask: Message => Future[Message],
    var paused: Boolean = false,
    var selectedTowerType: TowerType[_])
    extends ViewGameMenuController {

  override def setup(): Unit = {
    setSpacing()
    setupButtons()
    setupTowerDepot()
  }

  override def setSend(reference: Messages.Input => Unit): Unit = send = reference
  override def setAsk(reference: Message => Future[Message]): Unit = ask = reference

  override def isPaused: Boolean = paused

  override def anyTowerSelected(): Boolean =
    towerDepot.children.map(_.getStyleClass.contains("selected")).reduce(_ || _)

  override def unselectDepot(): Unit =
    towerDepot.children.foreach(_.getStyleClass.remove("selected"))

  override def getSelectedTowerType[B <: Bullet]: TowerType[B] =
    selectedTowerType.asInstanceOf[TowerType[B]]

  override def update(stats: GameStats): Unit = {
    lifeLabel.text = stats.life.toString
    moneyLabel.text = stats.wallet.toString
  }

  override def fillTowerStatus(tower: Tower[_], cell: Cell): Unit = Platform runLater {
    clearTowerStatus()
    if (currentCell == cell) {
      currentCell = outerCell
    } else {
      currentCell = cell
      Rendering a tower into towerStatus.children
      setTowerStatusPosition(tower)
      setTowerStatusRatio(tower)
      setTowerStatusSight(tower)
    }
  }

  override def clearTowerStatus(): Unit =
    towerStatus.children.clear()

  private def setSpacing(): Unit = {
    val space: Double = 10.0
    gameMenu.setSpacing(space)
    towerDepot.setSpacing(space)
  }

  private def setupButtons(): Unit =
    //exitButton.onMouseClicked = _ =>
    playButton.onMouseClicked = _ =>
      if (paused) {
        send(ResumeGame())
        paused = false
      } else {
        send(PauseGame())
        paused = true
      }

  private def setupTowerDepot[B <: Bullet](): Unit =
    TowerTypes.values.foreach { towerValue =>
      val tower: Tower[B] = towerValue.asInstanceOf[TowerType[B]].tower
      val renderedTower: Shape = Rendering a tower as single
      val towerBox: HBox = new HBox(renderedTower)
      towerBox.styleClass += "towerBox"
      towerBox.setCursor(Cursor.Hand)
      towerBox.onMousePressed = _ =>
        if (!paused) {
          if (!towerBox.styleClass.contains("selected")) {
            unselectDepot()
            towerBox.styleClass += "selected"
            selectedTowerType = towerValue.asInstanceOf[TowerType[B]]
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

  private def setTowerStatusPosition(tower: Tower[_]): Unit = {
    val cell: Cell = Constants.Maps.gameGrid.specificCell(tower.position)
    val box: HBox = new HBox()
    val key: Label = Label("Position: ")
    val value: Label = Label("(" + cell.x.toString + ", " + cell.y.toString + ")")
    box.children += key
    box.children += value
    towerStatus.children += box
  }

  private def setTowerStatusRatio(tower: Tower[_]): Unit = {
    val box: HBox = new HBox()
    val key: Label = Label("Shot ratio: ")
    val value: Label = Label(tower.shotRatio.toString)
    box.children += key
    box.children += value
    towerStatus.children += box
  }

  private def setTowerStatusSight(tower: Tower[_]): Unit = {
    val box: HBox = new HBox()
    val key: Label = Label("Sight range: ")
    val value: Label = Label(tower.sightRange.toString)
    box.children += key
    box.children += value
    towerStatus.children += box
  }
}
