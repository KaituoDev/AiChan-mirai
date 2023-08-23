package fun.kaituo.aichanmirai;

import java.util.ArrayList;
import java.util.List;

public class AiChanMiraiTimers {

    public static final AiChanMiraiTimers INSTANCE = new AiChanMiraiTimers();


    private int greetCounter = 0;

    public int getGreetCounter() {
        return greetCounter;
    }

    public void deductGreetCoolDown() {
        if (greetCounter == 2 || greetCounter == 1) {
            greetCounter -= 1;
        } else if (greetCounter > 2 || greetCounter < 0) {
            greetCounter = 0;
        }
        //AiChanMirai.INSTANCE.getLogger().info("Refreshed greet counter to " + greetCounter);
    }

    public void addGreetCoolDown() {
        if (greetCounter == 0 || greetCounter == 1) {
            greetCounter += 1;
        } else if (greetCounter > 2 || greetCounter < 0) {
            greetCounter = 0;
        }
    }


    private final List<String> responseInCoolDown = new ArrayList<>();

    public boolean checkResponseAvailability(String key) {
        for (String string : new ArrayList<>(responseInCoolDown)) {
            if (key.equals(string)) {
                return false;
            }
        }
        return true;
    }

    public void setResponseAvailable(String key) {
        responseInCoolDown.removeIf(key::equals);
    }

    public void setResponseUnavailable(String key) {
        if (checkResponseAvailability(key)) {
            responseInCoolDown.add(key);
        }
    }

}
