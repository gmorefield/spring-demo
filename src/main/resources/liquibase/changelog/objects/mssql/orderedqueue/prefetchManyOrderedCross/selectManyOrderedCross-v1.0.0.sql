CREATE OR ALTER PROCEDURE SELECT_MANY_ORDERED_CROSS
    @Count  INT,
    @Msg    VARCHAR(25) = ''
AS
BEGIN
    SET NOCOUNT ON
    SET XACT_ABORT ON;
    DECLARE @itemTable TABLE (id int);

    BEGIN TRY
        BEGIN TRANSACTION;
         EXEC sp_getapplock @Resource = 'prefetchManyOrdered', @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 60000;

        INSERT INTO @itemTable (id)
        SELECT TOP (@Count) s.id
          FROM orderedqueue o WITH (UPDLOCK)
         CROSS APPLY (
                 SELECT TOP 1 id, status
                   FROM orderedqueue i
                  WHERE i.order_id = o.order_id
                    AND i.status != 'C'
                  ORDER BY i.id
         ) AS s
         WHERE s.status = 'R'
           AND s.id = o.id
         ORDER BY s.id;

        UPDATE orderedqueue WITH (ROWLOCK, UPDLOCK)
           SET status = 'I',
               update_dt = getutcdate(),
               msg = (@Msg)
        OUTPUT inserted.id INTO @itemTable
         WHERE id IN (select id from @itemTable)
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