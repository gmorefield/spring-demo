CREATE OR ALTER PROCEDURE SELECT_NEXT
    @Msg VARCHAR(25)
AS
BEGIN
    SET NOCOUNT ON
    SET XACT_ABORT ON;
    DECLARE @NextId INTEGER

    BEGIN TRY
        BEGIN TRANSACTION

        -- Find the next queued item that is waiting to be processed
        SELECT TOP 1 @NextId = id
        FROM workqueue WITH (UPDLOCK, READPAST)
        WHERE status = 'R'
        ORDER BY id

        -- if we've found one, mark it as being processed
        IF @NextId IS NOT NULL
            UPDATE workqueue
            SET status = 'I',
                update_dt = getutcdate(),
                msg = (@Msg)
            WHERE id = @NextId

        COMMIT TRANSACTION

        -- If we've got an item from the queue, return to whatever is going to process it
        SELECT *
          FROM workqueue
         WHERE id = @NextId;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        THROW;
    END CATCH
END;