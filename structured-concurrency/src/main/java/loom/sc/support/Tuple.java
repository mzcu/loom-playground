package loom.sc.support;

public class Tuple {
    public record Tuple2<T1, T2>(T1 first, T2 second) {}
    public record Tuple3<T1, T2, T3>(T1 first, T2 second, T3 third) {}
    public record Tuple4<T1, T2, T3, T4>(T1 first, T2 second, T3 third, T4 fourth) {}
}
