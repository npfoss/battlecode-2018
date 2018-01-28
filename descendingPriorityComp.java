/****************/
/* REFACTOR ME! */
/****************/

import java.util.Comparator;

/*
decides who to shoot
*/
public class descendingPriorityComp implements Comparator<TargetUnit> {

	@Override
	public int compare(TargetUnit o1, TargetUnit o2) {
		if(o1.ID == o2.ID)
			return 0;
		if(o1.priority < o2.priority)
			return 1;
		if(o1.priority > o2.priority)
			return -1;
		if(o1.health > o2.health)
			return 1;
		if(o2.health > o1.health) 
			return -1;
		return Integer.compare(o1.ID,o2.ID);
	}
	
}
