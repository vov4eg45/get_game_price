package logic

import DB.DAO.DAOImpl
import cats.effect.IO
import cats.implicits._
import io.circe.{Json, parser}
import model.Game

import scala.io.Source

class MvideoLogic(dao: DAOImpl) extends Logic[IO] {

  def writeGamesToDb: IO[Seq[Boolean]] = {
    (for {
      ps <- getAllPsGames
      xbox <- getAllXGames
    } yield xbox++ps).flatMap(x => dao.writeSeqOfGames(x))
  }

  def findGame(title: String): IO[Seq[Game]] = {
    println("asd")
    for {
      _ <- writeGamesToDb
      game <- dao.getGameByTitle(title)
    } yield game
  }

  private def getProductIdsAndTitles(path: String): IO[List[(Int, String)]] = {
    val doc = IO(Source.fromFile(path)) // path = resources/productIds1.txt
    for {
      res <- doc
      json = parser.parse(res.mkString).getOrElse(Json.Null)
      id = (json \\ "productId").map(_.as[Int].getOrElse(0))
      title = (json \\ "modelName").map(_.toString)
    } yield id zip title
  }

  private def getAllProductIdsAndTitles(n: Int, path: String): IO[Seq[(Int, String)]] = {
    val seq = (1 to n).map{ n => getProductIdsAndTitles(path + n + ".txt") } // path = "resources/PS4/productIds"
    seq.foldLeft(IO[Seq[(Int, String)]]{Seq.empty[(Int, String)]})(
      (acc, nextIO) =>
        for {
          acc <- acc
          io <- nextIO
        } yield acc ++ io
    )
  }

  private def getAllPsGames = {
    val listTriple = for {
      ps4 <- getAllProductIdsAndTitles(6, "resources/PS4/productIds")
      ps5 <- getAllProductIdsAndTitles(1, "resources/PS5/productIds")
      all = ps4 ++ ps5
      doc = Source.fromURL(constructUrlForPrice(all.map(_._1).filter(_ != 0)))
      prices = (parser.parse(doc.mkString).getOrElse(Json.Null) \\ "basePrice").map(_.as[Int].getOrElse(0))
    } yield (all zip prices).map(x => (x._1._1, x._1._2, x._2))
    for {
      triple <- listTriple
    } yield triple.map(x => Game(x._2, List("PlaStation"), x._3, "MVideo"))
  }

  private def getAllXGames = {
    val listTriple = for {
      xbox <- getAllProductIdsAndTitles(3, "resources/XBOX/productIds")
      doc = Source.fromURL(constructUrlForPrice(xbox.map(_._1)))
      prices = (parser.parse(doc.mkString).getOrElse(Json.Null) \\ "basePrice").map(_.as[Int].getOrElse(0))
    } yield (xbox zip prices).map(x => (x._1._1, x._1._2, x._2))
    for {
      triple <- listTriple
    } yield triple.map(x => Game(x._2, List("Xbox"), x._3, "MVideo"))
  }

  private def constructUrlForPrice(ids: Seq[Int]): String = {
    val url1 = "https://www.mvideo.ru/bff/products/prices?productIds="
    val url2 = "&addBonusRubles=true&isPromoApplied=true"
    val mid = ids.mkString("%2C")
    url1 + mid + url2
  }
}