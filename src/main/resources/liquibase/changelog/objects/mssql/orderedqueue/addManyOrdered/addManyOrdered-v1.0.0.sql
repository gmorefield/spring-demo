CREATE OR ALTER PROCEDURE ADD_MANY_ORDERED
    @Count  INT,
    @Order  INT
AS
BEGIN
    SET NOCOUNT ON
    SET XACT_ABORT ON;

    BEGIN TRY
        DECLARE @RowsAdded int = 0, @OrderId int;
        WHILE (@RowsAdded < @Count)
        begin
            SELECT @OrderId = FLOOR(RAND()*(@Order));
            INSERT INTO orderedqueue (wid,order_id,status) VALUES (newid(),@OrderId,'R');
            SELECT @RowsAdded = @RowsAdded + 1;
            --if (@order > 10) set @order = 1;
        end;
        SELECT @RowsAdded;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        THROW;
    END CATCH

END;