package asyncGHS;

public class NeighborObject {
    Integer id;
    Float weight;

    public NeighborObject(Integer id, Float weight) {
        this.id = id;
        this.weight = weight;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight)
    {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "NeighborObject{" +
                "id=" + id +
                ", weight=" + weight +
                '}';
    }
}
