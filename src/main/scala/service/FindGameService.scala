package service

import DB.DAO.DAOImpl
import cats.effect.IO
import logic.{MvideoLogic, PsLogic, XboxLogic}
import model.Game

class FindGameService(mvideoLogic: MvideoLogic, psLogic: PsLogic, xboxLogic: XboxLogic, dao: DAOImpl) {

  def writeToDB: IO[Unit] =
    for {
      //_ <- mvideoLogic.writeGamesToDb
      _ <- psLogic.writeGamesToDb
      _ <- xboxLogic.writeGamesToDb
    } yield ()

  def findGame(title: String): IO[Seq[Game]] =
    for {
      games <- dao.getGameByTitle(title)
    } yield games.distinct

}