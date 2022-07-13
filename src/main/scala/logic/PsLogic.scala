package logic
import DB.DAO.DAOImpl
import cats.effect.IO
import cats.implicits._
import io.circe.{Json, parser}
import model.Game
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors._

class PsLogic(dao: DAOImpl) extends Logic[IO] {

  def writeGamesToDb: IO[Seq[Boolean]] = getGamesInfo.flatMap(game => dao.writeSeqOfGames(game))

  def findGame(title: String): IO[Seq[Game]] =
    for {
      gameX <- dao.getGameByTitle(title)
    } yield gameX

  val PS4url = "https://store.playstation.com/en-us/category/85448d87-aa7b-4318-9997-7d25f4d275a4/"
  val PS5url = "https://store.playstation.com/en-us/category/d71e8e6d-0940-4e03-bd02-404fc7d31a31/"

  val PS4pages = "$CategoryGrid:85448d87-aa7b-4318-9997-7d25f4d275a4:en-us:0:24.pageInfo"
  val PS5pages = "$CategoryGrid:d71e8e6d-0940-4e03-bd02-404fc7d31a31:en-us:0:24.pageInfo"

  private def getGamesInfo: IO[Seq[Game]] = {

    def getJsonFromHTML(path: String, elem: String) = {
      val browser = IO.pure(JsoupBrowser())
      browser.map{ browser =>
        val jsonString = (browser.get(path) >?> element(elem)).get.innerHtml
        parser.parse(jsonString).getOrElse(Json.Null)
      }
    }

    def getNumberOfPages(jsonIO: IO[Json], field: String) = {
      for {
        json <- jsonIO
        items = json.hcursor.downField("props").downField("apolloState").downField(field)
        pages = items.get[Int]("totalCount")
        size = items.get[Int]("size")
      } yield pages.getOrElse(-1) / size.getOrElse(1) + 1
    }

    def getGamesFromPage(jsonIO: IO[Json]) = {
      val t = for {
                json <- jsonIO
                items = json.hcursor.downField("props").downField("apolloState")
                name = (items.as[Json].getOrElse(Json.Null) \\ "name").drop(23) // 23 first "names" are always not useful
                price = items.as[Json].getOrElse(Json.Null) \\ "basePrice"
              } yield name.map(_.toString) zip listOfJsonToListOfPrice(price)
      for {
        list <- t
      } yield list.map{ case (t, p) => Game(t, List("PlaStation"), p, "PsStore") }
    }

    def getGamesFromAllPages(n: Int, url: String): IO[Seq[Game]] =
      List.range(1, n).parTraverse(m => getGamesFromPage(getJsonFromHTML(url + m, "#__NEXT_DATA__"))).map(_.flatten)

    val jsonPS4 = getJsonFromHTML(PS4url + 1, "#__NEXT_DATA__")
    val jsonPS5 = getJsonFromHTML(PS5url + 1, "#__NEXT_DATA__")

    for {
      numOfPages <- (getNumberOfPages(jsonPS4, PS4pages), getNumberOfPages(jsonPS5, PS5pages)).parMapN {(x, y) => (x, y)}
      games <- (getGamesFromAllPages(numOfPages._1, PS4url), getGamesFromAllPages(numOfPages._2, PS5url)).parMapN {(x, y) => x ++ y}
      //games <- List(getGamesFromAllPages(numOfPages._1, PS4url), getGamesFromAllPages(numOfPages._2, PS5url)).parSequence.map(_.flatten)
    } yield games

  }

  private def listOfJsonToListOfPrice(jsonList: List[Json]): List[Double] = {
    for {
      json <- jsonList
      price = json.toString().drop(2).dropRight(1).toDoubleOption
    } yield price.getOrElse(0.0)
  }

}