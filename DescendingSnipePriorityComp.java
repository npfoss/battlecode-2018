import java.util.Comparator;

public class DescendingSnipePriorityComp implements Comparator<TargetUnit> {

	@Override
	public int compare(TargetUnit o1, TargetUnit o2) {
		if(o1.ID == o2.ID)
			return 0;
		if(o1.snipePriority < o2.snipePriority)
			return 1;
		if(o1.snipePriority > o2.snipePriority)
			return -1;
		return Integer.compare(o1.ID,o2.ID);
	}
	
}
