package smile.math.distance

import scala.math.pow

/**
 * @author Beck Gaël
 *
 **/
class Vari extends BinaryDistance {

	override def distance(vector1: Array[Int], vector2: Array[Int]) : Double = {
		val (a,b,c,d) = BinaryUtils.contingencyTable(vector1, vector2)
		(b + c).toDouble / (4 * (a + b + c + d))
	}
	
}