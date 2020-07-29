package com.kedil.entities.contenttypes

/*

// Or just use the Live Template made for this

object %TypeName%Tables : IdTable<Long>() {
    private val snowflake = Snowflake(9)
    override val id = long("table_id").clientDefault { snowflake.next() }.entityId()
    // Properties

    override val primaryKey = PrimaryKey(id)
}

class %TypeName%Table(id: EntityID<Long>) : LongEntity(id), ContentEntity {
    companion object : LongEntityClass<%TypeName%Table>(%TypeName%Tables)

    var property by %TypeName%Tables.json

    val tableId
        get() = id.value

    override fun toSnippet() = Content%TypeName%Table(this.json)

    override fun delete() {
        val it = this
        transaction {
            it.delete()
        }
    }
}


class Content%TypeName%Table(
        // All properties, with @JsonProperty Context
) : ContentType {
    override fun createNew(newPosition: Long, newPage: Page): AdminContent {
       return transaction {
           val %typeName%Table = %TypeName%Table.new {
               this.property = this@Content%TypeName%Table.property
           }
           val str = PageStructure.new {
               contentType = ContentTypes.%TABLE_NAME_TYPE%
               contentId = %typeName%Table.tableId
               position = newPosition
               page = newPage
           }
           AdminContent(
                   str.pageStructureId.toString(),
                   %typeName%Table.toSnippet()
           )
       }
    }

    override fun edit(entity: ContentEntity): Boolean {
        if(entity !is %TypeName%Table) {
            return false;
        }
        transaction {
            entity.json = json
        }
        return true;
    }
}




 */


/*
In Addition, add Type-String to ContentTypes.kt in this form:


// Self-Explaining
    const val %TYPE_NAME%_TABLE = "%typename%table"

Don't forget to add the Type to the JsonSubTypes in ContentType.kt

JsonSubTypes.Type(Content%TypeName%Table::class, name = ContentTypes.%TYPE_NAME%_TABLE)

Also add it to the PageStructure content property:

fun content() : ContentEntity?  {
    return when(contentType) {
        ...
        ContentTypes.%Type_Name%_TABLE -> transaction { %TypeName%Table.findById(contentId) }
        ...
        else -> null
    }
}


*/


/*
Now you should be ready to use this new Type!
 */