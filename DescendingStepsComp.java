import java.util.Comparator;

public class DescendingStepsComp implements Comparator<CombatUnit> {
	@Override
	public int compare(CombatUnit o1, CombatUnit o2) {
		if(o1.ID == o2.ID)
			return 0;
		if(o1.stepsFromTarget < o2.stepsFromTarget)
			return 1;
		return -1;
	}
}
