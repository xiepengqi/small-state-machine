package com.xpq.stateMachine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SmallStateMachine<CONTEXT, EVENT, STATE> {
    public CONTEXT CONTEXT;
    public STATE STATE;
    public EVENT EVENT;
    public STATE NEXT_STATE;
    public STATE LAST_STATE;

    private Map<STATE, List<Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>> entryAction = new ConcurrentHashMap<>();
    private Map<STATE, List<Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>> exitAction = new ConcurrentHashMap<>();
    private Map<EVENT, Map<STATE, Map<STATE, List<Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>>>> eventTransitAction = new ConcurrentHashMap<>();
    private Map<EVENT, Map<STATE,List< Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>>> transitInitMap = new ConcurrentHashMap<>();

    private SmallStateMachine() {

    }

    public static <CONTEXT, EVENT, STATE> SmallStateMachine<CONTEXT, EVENT, STATE> builder(Class<CONTEXT> contextClass, Class<EVENT> eventClass, Class<STATE> stateClass) {
        return new SmallStateMachine<>();
    }

    @SafeVarargs
    public final SmallStateMachine<CONTEXT, EVENT, STATE> onEntry(STATE state, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> ...action){
        entryAction.put(state, Arrays.asList(action));
        return this;
    }

    @SafeVarargs
    public final SmallStateMachine<CONTEXT, EVENT, STATE> onExit(STATE state, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> ...action){
        exitAction.put(state, Arrays.asList(action));
        return this;
    }

    @SafeVarargs
    public final SmallStateMachine<CONTEXT, EVENT, STATE> defTransit(EVENT event, STATE from, STATE to, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> ...action){
        Map<STATE, List<Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>> map = eventTransitAction.computeIfAbsent(event, e -> new ConcurrentHashMap<>()).computeIfAbsent(from, f -> new ConcurrentHashMap<>());
        map.clear();
        map.put(to, Arrays.asList(action));
        return this;
    }

    @SafeVarargs
    public final SmallStateMachine<CONTEXT, EVENT, STATE> defTransit(EVENT event, STATE to, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> ...action){
        transitInitMap.computeIfAbsent(event, f -> new ConcurrentHashMap<>()).put(to, Arrays.asList(action));
        return this;
    }

    public STATE fire(CONTEXT context, EVENT event, STATE state) {
        SmallStateMachine<CONTEXT, EVENT, STATE> machine = new SmallStateMachine<>();
        machine.STATE = state;
        machine.EVENT = event;
        machine.CONTEXT = context;

        if (valid(context, event)) {
            if (state == null) {
                Map<STATE, List<Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>> transitAction = transitInitMap.get(event);
                if (transitAction != null) {
                    machine.NEXT_STATE = transitAction.keySet().stream().findFirst().orElse(null);
                    transitAction.values().stream().findFirst().orElse(new ArrayList<>()).forEach(item -> item.accept(machine));
                    machine.STATE = machine.NEXT_STATE;
                    machine.NEXT_STATE = null;
                    if (entryAction.containsKey(machine.STATE)) {
                        entryAction.get(machine.STATE).forEach(item -> item.accept(machine));
                    }
                }
            } else {
                process(machine);
            }
        }
        return machine.STATE;
    }

    private boolean valid(Object ...objs){
        boolean valid = true;
        for (Object obj : objs) {
            if (obj == null) {
                valid = false;
                break;
            }
        }
        return valid;
    }

    private void process(SmallStateMachine<CONTEXT, EVENT, STATE> machine) {
        Map<STATE, List<Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>> transitAction = new HashMap<>();
        if (eventTransitAction.containsKey(machine.EVENT)
                && eventTransitAction.get(machine.EVENT).containsKey(machine.STATE)) {
            transitAction = eventTransitAction.get(machine.EVENT).get(machine.STATE);
        }
        if (transitAction == null) {
            return;
        }
        machine.NEXT_STATE = transitAction.keySet().stream().findFirst().orElse(null);
        if (exitAction.containsKey(machine.STATE)) {
            exitAction.get(machine.STATE).forEach(item -> item.accept(machine));
        }
        transitAction.values().stream().findFirst().orElse(new ArrayList<>()).forEach(item -> item.accept(machine));
        machine.LAST_STATE = machine.STATE;
        machine.STATE = machine.NEXT_STATE;
        machine.NEXT_STATE = null;
        if (entryAction.containsKey(machine.STATE)) {
            entryAction.get(machine.STATE).forEach(item -> item.accept(machine));
        }
    }
}
