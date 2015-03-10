package vargovcik.peter.atasp_companionapp.uiaddons;

public interface AnalogPadInterface {
	public enum ANALOG_PAD{KEY_DOWN,KEY_UP}
	void analogPadEvent(float xPosition, float yPosition);
	void analogPadKeyEvent(ANALOG_PAD event);
}
