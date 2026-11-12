package br.edu.cesmac.frota_ops.tag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Contagem de veículos por tag. Tags se sobrepõem por natureza: um veículo pode pertencer
     * a várias tags, portanto a soma da série pode ser maior que o total da frota.
     */
    interface ContagemPorTag {
        String getTag();

        long getTotal();
    }

    /** Catálogo completo de tags, incluindo as que ainda não têm veículos (total zero). */
    @Query(value = "select t.nome as tag, count(vt.id_veiculo) as total "
            + "from tags t "
            + "left join veiculo_tags vt on vt.id_tag = t.id "
            + "group by t.id, t.nome "
            + "order by count(vt.id_veiculo) desc, t.nome asc", nativeQuery = true)
    List<ContagemPorTag> contarVeiculosPorTag();

    /** Mesma contagem, restrita aos veículos de um motorista (escopo do perfil MOTORISTA). */
    @Query(value = "select t.nome as tag, count(vt.id_veiculo) as total "
            + "from veiculo_tags vt "
            + "join tags t on t.id = vt.id_tag "
            + "join veiculo v on v.id = vt.id_veiculo "
            + "where v.id_motorista = :motoristaId "
            + "group by t.id, t.nome "
            + "order by count(vt.id_veiculo) desc, t.nome asc", nativeQuery = true)
    List<ContagemPorTag> contarVeiculosPorTagDoMotorista(@Param("motoristaId") Long motoristaId);
}
