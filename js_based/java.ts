export namespace lang{
    export type char = number;
    export type int = bigint;
    export type long = bigint;
    export type float = number;
    export type double = number;

    export interface Appendable{
        append(c:char)
        append(csq:CharSequence, start:int, end:int)
        append(csq:CharSequence)
    }

    export abstract class AutoCloseable{
        abstract close()
    }

    export function CharSequenceDefault(obj:Function){
        obj.prototype.chars= () => {

        }
    }
    
    export interface  CharSequence extends CharSequenceDefault{
        charAt(index:int):char
        //chars:()=>utils.stream.IntStream
        
    }

    @CharSequenceDefault
    class d implements CharSequence{
        constructor(){
            this.char()
        }

        charAt(index: bigint): number {
        }
    }

    let a = new d()

    export class Class extends Object implements CharSequence{

    }
}

export namespace utils{
    export namespace stream{
        export interface IntStream{
            
        }

        export namespace IntStream{
            export class Builder{

            }
        }
    }
}