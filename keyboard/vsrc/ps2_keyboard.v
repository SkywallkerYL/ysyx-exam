module ps2_keyboard(clk,clrn,ps2_clk,ps2_data,data,ps2_count,
                    ready,nextdata_n,overflow,flagen);
    input clk,clrn,ps2_clk,ps2_data;
    input nextdata_n;
    output [7:0] data;    
    output reg [11:0] ps2_count;
    output reg ready;
    output reg overflow;     // fifo overflow
    output  flagen;// 用来只是显示管是否显示键码
    // internal signal, for test
    reg [9:0] buffer;        // ps2_data bits
    reg [7:0] fifo[7:0];     // data fifo
    reg [2:0] w_ptr,r_ptr;   // fifo write and read pointers
    reg [3:0] count;  // count ps2_data bits
    // detect falling edge of ps2_clk
    reg [2:0] ps2_clk_sync;

    always @(posedge clk) begin
        ps2_clk_sync <=  {ps2_clk_sync[1:0],ps2_clk};
    end

    wire sampling = ps2_clk_sync[2] & ~ps2_clk_sync[1];
    assign flagen = ( fifo[r_ptr-3'b001]!=8'hF0 &&fifo[r_ptr-3'b010]!=8'hF0 ) ;
    always @(posedge clk) begin
        if (clrn == 0) begin // reset
            count <= 0; w_ptr <= 0; r_ptr <= 0; overflow <= 0; ready<= 0;
            ps2_count <= 0;
        end
        else begin
            if ( ready ) begin // read to output next data
                //不是码字时也不要加
                if( (fifo[r_ptr]==8'h00)||fifo[r_ptr] == 8'hF0||(fifo[r_ptr-3'b001]==8'hF0)||(fifo[r_ptr]==fifo[r_ptr-3'b001]&&fifo[r_ptr-3'b010]!=8'hF0)) begin
                     ps2_count <= ps2_count;
                    end
                     else begin 
                        ps2_count <= ps2_count + 1'b1;
                        $display("pscount %d", ps2_count);
                     end
                     /*
                if( (fifo[r_ptr]==8'h00)||fifo[r_ptr] == 8'hF0||(fifo[r_ptr-3'b001]==8'hF0)) begin
                    flagen <= 1'b0;
                end
                else begin 
                    flagen<=1'b1;
                end
                */
                if(nextdata_n == 1'b0) //read next data
                begin

                    r_ptr <= r_ptr + 3'b1;
                    if(w_ptr==(r_ptr+1'b1)) //empty
                        ready <= 1'b0;
                end
            end
            if (sampling) begin
              if (count == 4'd10) begin
                if ((buffer[0] == 0) &&  // start bit
                    (ps2_data)       &&  // stop bit
                    (^buffer[9:1])) begin      // odd  parity
                    fifo[w_ptr] <= buffer[8:1];  // kbd scan code
                     $display("receive %x", buffer[8:1]);
                     /*
                     $display("fifo_wptr %x", fifo[w_ptr][7:0]);
                     $display("fifo_rptr %x", fifo[r_ptr][7:0]);
                     $display("w_ptr %d", w_ptr[2:0]);
                     $display("fifo_1 %x", fifo[0][7:0]);
                     $display("fifo_2 %x", fifo[1][7:0]);
                     $display("fifo_3 %x", fifo[2][7:0]);
                     $display("fifo_4 %x", fifo[3][7:0]);
                     $display("fifo_5 %x", fifo[4][7:0]);
                     $display("fifo_6 %x", fifo[5][7:0]);
                     $display("fifo_7 %x", fifo[6][7:0]);
                     $display("fifo_8 %x", fifo[7][7:0]);
                     */
                     
                     
                     
                    w_ptr <= w_ptr+3'b1;
                    ready <= 1'b1;//queue is not empty,data to process
                    overflow <= overflow | (r_ptr == (w_ptr + 3'b1));
                end
                count <= 0;     // for next
              end else begin
                buffer[count] <= ps2_data;  // store ps2_data
                count <= count + 3'b1;
              end
            end
        end
    end
    assign data = fifo[r_ptr-3'b001]; //always set output data

endmodule
