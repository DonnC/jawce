import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timeFormat'
})
export class TimeFormatPipe implements PipeTransform {

  transform(value: unknown, ...args: unknown[]): unknown {
    if (!value) return '';

    // Split the date and time
    // @ts-ignore
    const dateTimeParts = value.split(' ');
    if (dateTimeParts.length !== 3) return '';

    // Extract the time and period parts
    const timePart = dateTimeParts[1];
    const periodPart = dateTimeParts[2];

    return `${timePart} ${periodPart}`;
  }

}
