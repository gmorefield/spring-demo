CREATE OR ALTER PROCEDURE OUTPUT_MANY_ORDERED_SUB
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

        UPDATE orderedqueue WITH (UPDLOCK)
           SET status = 'I',
               update_dt = getutcdate(),
               msg = (@Msg)
        OUTPUT inserted.id INTO @itemTable
         WHERE id IN (
                   SELECT TOP (@Count) o.id
                     FROM orderedqueue o WITH (UPDLOCK)
                    WHERE o.status = 'R'
                      AND o.id = (
                            SELECT MIN(i.id)
                              FROM orderedqueue i
                             WHERE i.order_id = o.order_id
                               AND i.status != 'C'
                          )
                    ORDER BY o.id
               )
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