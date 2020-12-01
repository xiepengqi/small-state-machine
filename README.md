# small-state-machine

A small state machine for java.

### example

```java
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

        machine.onEntry(OrderStatus.init, m -> System.out.println("entryInit"))
                .onExit(OrderStatus.init, m -> System.out.println("exitInit"))
        .onEntry(OrderStatus.waitPay, m -> System.out.println("entryWaitPay"))
        ;

        machine.defTransit(Event.pay_create, OrderStatus.init, m -> System.out.println("to init"))
                .defTransit(Event.pay_confirm, OrderStatus.init, OrderStatus.success, m -> System.out.println("pay_confirm init to success"))
                .defTransit(Event.pay_confirm, OrderStatus.init, OrderStatus.waitPay, m -> System.out.println("pay_confirm init to waitPay"))
                .defTransit(Event.pay_confirm, OrderStatus.waitPay, OrderStatus.success, m -> System.out.println("pay_confirm waitPay to success"))
        ;

        machine.fire(order, Event.pay_create, null);
        machine.fire(order, Event.pay_confirm, OrderStatus.init);
    }
}
```
