package com.xpq.stateMachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SmallStateMachine<CONTEXT, EVENT, STATE> {
    public CONTEXT context;
    public STATE state;
    public EVENT event;
    public STATE nextState;

    private Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> allEntryAction;
    private Map<STATE, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>> entryAction = new ConcurrentHashMap<>();
    private Map<EVENT, Map<STATE, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>> eventEntryAction = new ConcurrentHashMap<>();

    private Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> allExitAction;
    private Map<STATE, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>> exitAction = new ConcurrentHashMap<>();
    private Map<EVENT, Map<STATE, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>> eventExitAction = new ConcurrentHashMap<>();

    private Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> allTransitAction;
    private Map<STATE, Map<STATE, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>> transitAction = new ConcurrentHashMap<>();
    private Map<EVENT, Map<STATE, Map<STATE, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>>> eventTransitAction = new ConcurrentHashMap<>();

    private SmallStateMachine() {

    }

    public static <CONTEXT, EVENT, STATE> SmallStateMachine<CONTEXT, EVENT, STATE> builder(Class<CONTEXT> contextClass, Class<EVENT> eventClass, Class<STATE> stateClass) {
        return new SmallStateMachine<>();
    }

    public SmallStateMachine<CONTEXT, EVENT, STATE> onEntry(Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> action){
        allEntryAction = action;
        return this;
    }
    public SmallStateMachine<CONTEXT, EVENT, STATE> onEntry(STATE state, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> action){
        entryAction.put(state, action);
        return this;
    }
    public SmallStateMachine<CONTEXT, EVENT, STATE> onEntry(EVENT event, STATE state, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> action){
        eventEntryAction.computeIfAbsent(event, e -> new ConcurrentHashMap<>()).put(state, action);
        return this;
    }

    public SmallStateMachine<CONTEXT, EVENT, STATE> onExit(Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> action){
        allExitAction = action;
        return this;
    }
    public SmallStateMachine<CONTEXT, EVENT, STATE> onExit(STATE state, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> action){
        exitAction.put(state, action);
        return this;
    }
    public SmallStateMachine<CONTEXT, EVENT, STATE> onExit(EVENT event, STATE state, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> action){
        eventExitAction.computeIfAbsent(event, e -> new ConcurrentHashMap<>()).put(state, action);
        return this;
    }
    public SmallStateMachine<CONTEXT, EVENT, STATE> onTransit(Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> action){
        allTransitAction = action;
        return this;
    }
    public SmallStateMachine<CONTEXT, EVENT, STATE> onTransit(STATE from, STATE to, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> action){
        transitAction.computeIfAbsent(from, f -> new ConcurrentHashMap<>()).put(to, action);
        return this;
    }

    public SmallStateMachine<CONTEXT, EVENT, STATE> transit(EVENT event, STATE from, STATE to, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> action){
        Map<STATE, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>> map = eventTransitAction.computeIfAbsent(event, e -> new ConcurrentHashMap<>()).computeIfAbsent(from, f -> new ConcurrentHashMap<>());
        map.clear();
        map.put(to, action);
        return this;
    }

    public STATE fire(CONTEXT context, EVENT event, STATE initState) {
        SmallStateMachine<CONTEXT, EVENT, STATE> machine = new SmallStateMachine<>();
        machine.state = initState;
        machine.event = event;
        machine.context = context;
        invokeAction(machine, allEntryAction, entryAction, eventEntryAction);
        process(machine);
        return machine.state;
    }

    private void process(SmallStateMachine<CONTEXT, EVENT, STATE> machine) {
        List<Map<STATE, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>> transits = new ArrayList<>();
        if (eventTransitAction.containsKey(machine.event)
                && eventTransitAction.get(machine.event).containsKey(machine.state)) {
            transits.add(eventTransitAction.get(machine.event).get(machine.state));
            if (transitAction.containsKey(machine.state)
                    && transitAction.get(machine.state).containsKey(transits.get(0).keySet().stream().findFirst().orElse(null))) {
                transits.add(transitAction.get(machine.state));
            }
            Collections.reverse(transits);
        }

        machine.nextState = transits.get(0).keySet().stream().findFirst().orElse(machine.state);
        if (allTransitAction != null) {
            allTransitAction.accept(machine);
        }
        transits.forEach(transit -> transit.values().stream().findFirst().orElse(m -> {}).accept(machine));
        invokeAction(machine, allExitAction, exitAction, eventExitAction);
        machine.state = machine.nextState;
        invokeAction(machine, allEntryAction, entryAction, eventEntryAction);
        machine.nextState = null;
    }

    private void invokeAction(SmallStateMachine<CONTEXT, EVENT, STATE> machine,
                              Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>> action,
                              Map<STATE, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>> actions,
                              Map<EVENT, Map<STATE, Consumer<SmallStateMachine<CONTEXT, EVENT, STATE>>>> eventActions) {
        if (action != null) {
            action.accept(machine);
        }
        if (actions.containsKey(machine.state)) {
            actions.get(machine.state).accept(machine);
        }
        if (eventActions.containsKey(machine.event)
                && eventActions.get(machine.event).containsKey(machine.state)) {
            eventActions.get(machine.event).get(machine.state).accept(machine);
        }
    }
}
