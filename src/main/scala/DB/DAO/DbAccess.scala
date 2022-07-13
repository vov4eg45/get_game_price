package DB.DAO

import model.Game

trait DbAccess[F[_]] {
  def getGameByTitle(title: String): F[Seq[Game]]
  def getGameByPlatform(platform: String): F[Seq[Game]]
  def writeGame(game: Game): F[Boolean]
}