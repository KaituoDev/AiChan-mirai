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
        greetCounter -= 1;
        if (greetCounter < 0) {
            greetCounter = 0;
        }
    }

    public void addGreetCoolDown() {
        greetCounter += 1;
        if (greetCounter > 2) {
            greetCounter = 2;
        }
    }


    private final List<String> responseInCoolDown = new ArrayList<>();

    public boolean checkResponseAvailability(String key) {
        return !responseInCoolDown.contains(key);
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
