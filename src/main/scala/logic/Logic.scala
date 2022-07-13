package logic

import model.Game

trait Logic[F[_]] {
  def writeGamesToDb: F[Seq[Boolean]]
  def findGame(title: String): F[Seq[Game]]
}