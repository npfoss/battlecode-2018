/****************/
/* REFACTOR ME! */
/****************/

import java.util.Comparator;

/*
just a comparator used for organizing combat--(to move in an intelligent order)
// TODO: may want to take unit type into account (like knights first always to rush in?)
*/
public class AscendingStepsComp implements Comparator<CombatUnit> {

	@Override
	public int compare(CombatUnit o1, CombatUnit o2) {
		if(o1.ID == o2.ID)
			return 0;
		if(o1.stepsFromTarget > o2.stepsFromTarget)
			return 1;
		else if(o2.stepsFromTarget > o1.stepsFromTarget)
			return -1;
		return Integer.compare(o1.ID, o2.ID);
	}
	
}
