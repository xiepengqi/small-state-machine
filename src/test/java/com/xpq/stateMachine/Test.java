package com.xpq.stateMachine;


public class Test {
    enum OrderStatus {
        init, waitPay, success, failed, exception, close, refund
        ;
    }

    enum Event {
        pay_create, pay_confirm, callback_confirm, job_close,
        refund,
        ;
    }

    static class Order{
        String orderNo;
        OrderStatus status;
    }

    public static void main(String[] args) {
        Order order = new Order();
        order.orderNo = "test123";

        SmallStateMachine<Order, Event, OrderStatus> machine = SmallStateMachine.builder(Order.class, Event.class, OrderStatus.class);

        machine.onEntry(m -> System.out.println(m.context.orderNo + " entry " + m.state))
                .onExit(m -> System.out.println("exit " + m.state))
                .onEntry(OrderStatus.init, m -> System.out.println("entryInit"))
                .onExit(OrderStatus.init, m -> System.out.println("exitInit"))
                .onExit(Event.pay_create, OrderStatus.init, m -> System.out.println("pay_create exit init"))
                .onTransit(OrderStatus.init, OrderStatus.waitPay, m -> System.out.println("init to waitPay"))
        ;
        machine.transit(Event.pay_create, OrderStatus.init, OrderStatus.success, m -> System.out.println("pay_create init to success"))
                .transit(Event.pay_create, OrderStatus.init, OrderStatus.waitPay, m -> System.out.println("pay_create init to waitPay"))
                .transit(Event.pay_create, OrderStatus.waitPay, OrderStatus.success, m -> System.out.println("pay_create waitPay to success"))
                .transit(Event.pay_create, OrderStatus.success, OrderStatus.init, m -> System.out.println("pay_create success to init"))
        ;

        System.out.println(machine.fire(order, Event.pay_create, OrderStatus.init));
    }
}
