CREATE OR ALTER PROCEDURE SELECT_MANY_ORDERED_JOIN
    @Count  INT,
    @Msg    VARCHAR(25) = ''
AS
BEGIN
    SET NOCOUNT ON
    SET XACT_ABORT ON;
    DECLARE @itemTable TABLE (id int);

    BEGIN TRY
        BEGIN TRANSACTION;
--        EXEC sp_getapplock @Resource = 'prefetchManyOrdered', @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 60000;

        INSERT INTO @itemTable (id)
        SELECT TOP (@Count) o.id
          FROM orderedqueue o WITH (UPDLOCK)
          LEFT JOIN orderedqueue o2
            ON o.order_id = o2.order_id
           AND o2.status in ( 'I', 'E' )
         WHERE o.status = 'R'
           AND o2.id IS NULL
         ORDER BY o.id

         UPDATE orderedqueue WITH (UPDLOCK)
           SET status = 'I',
               update_dt = getutcdate(),
               msg = (@Msg)
         WHERE id IN (SELECT id FROM @itemTable)
           AND status = 'R';

        COMMIT TRANSACTION;

        -- If we've got an item from the queue, return to whatever is going to process it
        SELECT *
          FROM orderedqueue
         WHERE id IN (SELECT id FROM @itemTable)
         ORDER BY id;

    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        THROW;
    END CATCH

END;