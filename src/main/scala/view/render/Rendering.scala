package view.render

import javafx.scene.effect.ImageInput
import javafx.scene.image.Image
import javafx.scene.paint.ImagePattern
import model.entities.Entities.Entity
import model.entities.balloons.balloontypes.BalloonDecorations.BalloonDecoration
import model.entities.balloons.balloontypes.CamoBalloons.Camo
import model.entities.bullets.Bullets.Bullet
import model.entities.towers.Towers.Tower
import model.maps.Cells.{ Cell, GridCell }
import model.maps.Grids.Grid
import model.maps.Tracks.Directions.RIGHT
import model.maps.Tracks.Track
import scalafx.scene.effect.{ Blend, BlendMode }
import scalafx.scene.layout.Region
import scalafx.scene.paint.Color
import scalafx.scene.shape.{ Ellipse, Rectangle, Shape }
import utils.Constants.Screen.cellSize
import view.render.Drawings._
import view.render.Renders.{ renderSingle, Rendered, ToBeRendered }

import scala.language.{ implicitConversions, reflectiveCalls }

/**
 * Object that simulates a DSL for rendering logic entities as shapes for a scalafx pane.
 */
object Rendering {

  val drawing: Drawing = Drawing(GameDrawings())
  val defaultWidth: Double = 400.0
  val defaultHeight: Double = 200.0

  /** Renders a [[Grid]] with grass drawings. */
  def a(grid: Grid): ToBeRendered = Rendered {
    grid.cells map { cell =>
      val rect: Shape = Rendering a cell
      rect.setFill(drawing the Grass)
      rect
    }
  }

  def a(entity: Entity): ToBeRendered = an(entity)

  /** Renders an [[Entity]] with its corresponding drawing. */
  def an(entity: Entity): ToBeRendered = Rendered {
    val rectangle: Rectangle = Rectangle(
      entity.position.x - entity.boundary._1 / 2,
      entity.position.y - entity.boundary._2 / 2,
      entity.boundary._1,
      entity.boundary._2
    )
    rectangle.setFill(drawing the Item(entity))
    entity match {
      case bullet: Bullet =>
        rectangle.rotate = Math.atan2(bullet.speed.y, bullet.speed.x) * 180 / Math.PI
      case tower: Tower[_] =>
        rectangle.rotate = Math.atan2(tower.direction.y, tower.direction.x) * 180 / Math.PI
        rectangle.styleClass += "tower"
      case decoration: BalloonDecoration =>
        val blend: Blend = new Blend()
        val effect: ImagePattern = decoration match {
          case _: Camo => drawing the CamoPattern
        }
        blend.setTopInput(new ImageInput(effect.getImage, rectangle.x.value, rectangle.y.value))
        blend.setMode(BlendMode.Difference)
        rectangle.setEffect(blend)
      case _ =>
    }
    rectangle
  }

  /** Renders the sight range of a [[Tower]]. */
  def sightOf(tower: Tower[_]): ToBeRendered = Rendered {
    val range: Ellipse =
      Ellipse(tower.position.x, tower.position.y, tower.sightRange, tower.sightRange)
    range.opacity = 0.3
    range.setFill(Color.LightGray)
    range
  }

  /** Renders a [[Cell]] just with a square [[Shape]]. */
  def a(cell: Cell): Shape =
    Rectangle(cell.x * cellSize, cell.y * cellSize, cellSize, cellSize)

  /** Renders a [[Track]] as a sequence of road drawings. */
  def a(track: Track): ToBeRendered = Rendered {
    track.cells
      .prepended(GridCell(-1, 0, RIGHT))
      .sliding(2)
      .map { couple =>
        val dir: String = couple.head.direction.toString + "-" + couple.last.direction.toString
        val cell: Cell = couple.last
        val rect: Shape = Rendering a cell
        rect.setFill(drawing the Road(dir))
        rect
      }
      .toSeq
  }

  /** Renders a [[ImagePattern]] just with a square [[Shape]]. */
  def a(image: ImagePattern): ToBeRendered = Rendered {
    val rectangle: Rectangle = Rectangle(defaultWidth, defaultHeight)
    rectangle.setFill(image)
    rectangle
  }

  /** Utility method for generating a [[Rectangle]] with the specified picture path. */
  def forInput(width: Double, height: Double, path: String): Shape = {
    val rectangle: Rectangle = Rectangle(width, height)
    rectangle.setFill(new ImagePattern(new Image(path)))
    rectangle
  }

  def setLayout(region: Region, width: Double, height: Double): Unit = {
    region.maxWidth = width
    region.minWidth = width
    region.maxHeight = height
    region.minHeight = height
  }

}
