package DB.DAO

import cats.effect.IO
import cats.implicits._
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import model.Game

class DAOImpl(implicit xa: doobie.Transactor[IO]) extends DbAccess[IO] {

  override def getGameByTitle(title: String): IO[Seq[Game]] = {
    val title2 = s"%$title%"
    sql""" SELECT * FROM gameInfo WHERE LOWER(title) LIKE $title2;""".query[Game].to[Seq].transact(xa)
  }

  override def getGameByPlatform(platform: String): IO[Seq[Game]] =
    sql"""
      SELECT * FROM gameInfo
      WHERE $platform = ANY(platform)
      ;""".query[Game].to[Seq].transact(xa)

  override def writeGame(game: Game): IO[Boolean] = {
    //println("1")
    sql"INSERT INTO gameInfo(title, platforms, price, source) VALUES ($game);".update.run.transact(xa).map(_ > 0)
  }

  def writeSeqOfGames(seq: Seq[Game]): IO[Seq[Boolean]] = {
    //println(seq)
    seq.map(writeGame).parSequence
  }

}