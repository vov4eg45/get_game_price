package logic
import DB.DAO.DAOImpl
import cats.effect.IO
import cats.implicits._
import io.circe.Json
import io.circe.parser.parse
import model.Game

import scala.io.Source.fromURL

class XboxLogic(dao: DAOImpl) extends Logic[IO] {

  def writeGamesToDb: IO[Seq[Boolean]] = {
    for {
      games <- getGamesInfo
      res <- dao.writeSeqOfGames(games)
    } yield res
  }

  def findGame(title: String): IO[Seq[Game]] =
    for {
      gameX <- dao.getGameByTitle(title)
    } yield gameX

  private val seqOfURLs: Seq[String] = Seq(
    "https://catalog.gamepass.com/sigls/v2?id=f6f1f99f-9b49-4ccd-b3bf-4d9767a77f5e&language=ru-ru&market=RU",
    "https://catalog.gamepass.com/sigls/v2?id=b8900d09-a491-44cc-916e-32b5acae621b&language=ru-ru&market=RU",
    "https://catalog.gamepass.com/sigls/v2?id=a884932a-f02b-40c8-a903-a008c23b1df1&language=ru-ru&market=RU",
    "https://catalog.gamepass.com/sigls/v2?id=79fe89cf-f6a3-48d4-af6c-de4482cf4a51&language=ru-ru&market=RU",
    "https://catalog.gamepass.com/sigls/v2?id=1d33fbb9-b895-4732-a8ca-a55c8b99fa2c&language=ru-ru&market=RU",
    "https://catalog.gamepass.com/sigls/v2?id=609d944c-d395-4c0a-9ea4-e9f39b52c1ad&language=ru-ru&market=RU",
    "https://catalog.gamepass.com/sigls/v2?id=29a81209-df6f-41fd-a528-2ae6b91f719c&language=ru&market=ru"
  )

  private def getID(seq: Seq[String]): IO[Seq[String]] = {
    def readAndParse(url: String): IO[Seq[String]] = {
      val doc = fromURL(url)
      IO.pure{(parse(doc.mkString).getOrElse(Json.Null) \\ "id").map(_.toString).foldRight(Seq.empty[String])(_ +: _)}
    }

    seq.parTraverse(readAndParse).map(_.flatten)
  }

  private def getGameInfo(gameID: String): IO[Game] = {
    val id = gameID.filter(_ != '"')
    val doc = fromURL(s"https://displaycatalog.mp.microsoft.com/v7.0/products?bigIds=$id&market=RU&languages=ru-ru&MS-CV=DGU1mcuYo0WMMp+F.1")
    val gameJson = IO { parse(doc.mkString).getOrElse(Json.Null) }
    for {
      json <- gameJson
      title = (json \\ "ProductTitle").headOption match {
        case Some(value) => value.toString
        case None => "none"
      }
      platform = (json \\ "PlatformName").distinct.map(_.toString.filter(c => c != '/' && c != '"'))
      price = (json \\ "ListPrice").distinct.map(_.as[Double].getOrElse(0.0)).find(_ != 0.0) match {
        case Some(value) => value
        case None => 0.0
      }
    } yield Game(title, platform, price, "XBoxStore")
  }

  private def getGamesInfo: IO[Seq[Game]] = {
    (for {
      ids <- getID(seqOfURLs)
      t = ids.map(getGameInfo).parSequence
    } yield t).flatten
  }

}