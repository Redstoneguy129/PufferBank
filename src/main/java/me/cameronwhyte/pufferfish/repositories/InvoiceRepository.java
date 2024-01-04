package me.cameronwhyte.pufferfish.repositories;

import me.cameronwhyte.pufferfish.entity.Invoice;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface InvoiceRepository extends CrudRepository<Invoice, UUID> {
}
